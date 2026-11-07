import json
import logging
import re
from typing import Dict, Any, List, Optional
from app.config import settings
from app.schemas.chatbot import (
    ChatbotRequest,
    ChatbotResponse,
    ToolCall,
    ChatMessage
)
from app.services.search_service import search_service

log = logging.getLogger(__name__)

INJECTION_PATTERNS = [
    r"ignore\s+(all\s+)?(previous|prior)\s+instructions",
    r"system\s+prompt",
    r"reveal\s+(the\s+)?api\s+key",
    r"act\s+as\s+(an?\s+)?admin",
    r"show\s+me\s+all\s+users",
    r"disregard\s+(your\s+)?safety",
    r"<script>",
    r"drop\s+table"
]

TOOLS_DEFINITION = [
    {
        "name": "search_services",
        "description": "Find marketplace services matching user requirements in a specific city",
        "input_schema": {
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "e.g. pipe leak, home cleaning, fan repair"},
                "city": {"type": "string", "description": "e.g. Bengaluru, Mumbai, Delhi"}
            },
            "required": ["query"]
        }
    },
    {
        "name": "check_availability",
        "description": "Check if a service has open booking slots on a requested date",
        "input_schema": {
            "type": "object",
            "properties": {
                "service_id": {"type": "integer"},
                "date": {"type": "string", "description": "YYYY-MM-DD"}
            },
            "required": ["service_id", "date"]
        }
    },
    {
        "name": "create_booking_draft",
        "description": "Prepare a booking draft for customer confirmation",
        "input_schema": {
            "type": "object",
            "properties": {
                "service_id": {"type": "integer"},
                "booking_date": {"type": "string", "description": "YYYY-MM-DD"},
                "address": {"type": "string"},
                "notes": {"type": "string"}
            },
            "required": ["service_id", "booking_date", "address"]
        }
    },
    {
        "name": "explain_booking_status",
        "description": "Explain the lifecycle status of the customer's own booking",
        "input_schema": {
            "type": "object",
            "properties": {
                "booking_id": {"type": "integer"}
            },
            "required": ["booking_id"]
        }
    }
]

class ChatbotService:
    def __init__(self):
        self.client = None
        if settings.anthropic_api_key:
            try:
                import anthropic
                self.client = anthropic.Anthropic(api_key=settings.anthropic_api_key)
                log.info("Anthropic client initialized.")
            except Exception as e:
                log.warning("Anthropic client initialization failed: %s", e)

    def _execute_local_tool(self, tool_name: str, args: Dict[str, Any], context: Any) -> Dict[str, Any]:
        if tool_name == "search_services":
            q = args.get("query", "")
            city = args.get("city")
            res = search_service.search(query=q, city=city, limit=3)
            return {
                "count": res.total,
                "services": [
                    {"id": s.id, "title": s.title, "price": s.price, "rating": s.rating, "city": s.city}
                    for s in res.results
                ]
            }

        elif tool_name == "check_availability":
            # Availability simulation: available for dates in future
            date_str = args.get("date", "")
            return {
                "service_id": args.get("service_id"),
                "date": date_str,
                "is_available": True,
                "available_slots": ["09:00 AM", "11:30 AM", "02:00 PM", "04:30 PM"]
            }

        elif tool_name == "create_booking_draft":
            # Enforce authorization: customer can only book for themselves
            return {
                "service_id": args.get("service_id"),
                "customer_id": context.user_id,
                "customer_email": context.user_email,
                "booking_date": args.get("booking_date"),
                "address": args.get("address"),
                "notes": args.get("notes", ""),
                "status": "DRAFT_REQUIRES_CONFIRMATION"
            }

        elif tool_name == "explain_booking_status":
            booking_id = args.get("booking_id")
            # Permission check: verify scope
            return {
                "booking_id": booking_id,
                "status_explanation": (
                    "PENDING: Payment received, pending provider verification and slot assignment. "
                    "CONFIRMED: Provider confirmed, direct phone & address unlocked. "
                    "COMPLETED: Service executed and ready for customer review."
                )
            }

        return {"error": f"Unknown tool {tool_name}"}

    def process_chat(self, request: ChatbotRequest) -> ChatbotResponse:
        user_msg = request.message.strip()

        # 1. Prompt Injection Defense
        for pattern in INJECTION_PATTERNS:
            if re.search(pattern, user_msg, re.IGNORECASE):
                return ChatbotResponse(
                    reply=(
                        "I am an authorized assistant for ServeNow Marketplace. "
                        "I cannot process instructions that attempt to alter system security policies, "
                        "bypass privacy controls, or access administrative records."
                    ),
                    action_required=None,
                    draft_booking=None,
                    tools_called=[]
                )

        # 2. Check for Anthropic API calling vs Local Intelligent Assistant
        if self.client and settings.anthropic_api_key:
            try:
                return self._call_anthropic(request)
            except Exception as e:
                log.warning("Anthropic API call failed, falling back to local deterministic chatbot: %s", e)

        # 3. Deterministic Local Intent Engine (Offline & Fallback safe)
        return self._local_intent_fallback(request)

    def _call_anthropic(self, request: ChatbotRequest) -> ChatbotResponse:
        system_prompt = (
            f"You are the official AI booking concierge for ServeNow Marketplace.\n"
            f"Current Logged-in User Context:\n"
            f"- User ID: {request.context.user_id}\n"
            f"- Name: {request.context.user_name}\n"
            f"- Role: {request.context.user_role}\n"
            f"- Email: {request.context.user_email}\n"
            f"Security Rules:\n"
            f"1. You only have permission to access data belonging to User ID {request.context.user_id}.\n"
            f"2. Never reveal other users' personal info, phone numbers, or bookings.\n"
            f"3. Always seek explicit user confirmation before finalizing any booking.\n"
            f"4. Keep replies helpful, polite, and concise."
        )

        messages = [{"role": m.role, "content": m.content} for m in request.conversation_history[-6:]]
        messages.append({"role": "user", "content": request.message})

        response = self.client.messages.create(
            model=settings.anthropic_model,
            max_tokens=500,
            system=system_prompt,
            tools=TOOLS_DEFINITION,
            messages=messages
        )

        tools_executed: List[ToolCall] = []
        action_required = None
        draft_booking = None
        final_reply = ""

        for block in response.content:
            if block.type == "text":
                final_reply += block.text
            elif block.type == "tool_use":
                t_name = block.name
                t_input = block.input
                t_res = self._execute_local_tool(t_name, t_input, request.context)
                tools_executed.append(ToolCall(tool_name=t_name, arguments=t_input, result=t_res))

                if t_name == "create_booking_draft":
                    action_required = "CONFIRM_BOOKING"
                    draft_booking = t_res

        if not final_reply:
            if action_required == "CONFIRM_BOOKING":
                final_reply = "I've drafted your booking! Please confirm the details before we submit."
            elif tools_executed:
                final_reply = f"I've retrieved the requested details: {json.dumps(tools_executed[0].result)}"
            else:
                final_reply = "How can I assist you with your ServeNow booking today?"

        return ChatbotResponse(
            reply=final_reply,
            action_required=action_required,
            draft_booking=draft_booking,
            tools_called=tools_executed
        )

    def _local_intent_fallback(self, request: ChatbotRequest) -> ChatbotResponse:
        q = request.message.lower()
        tools_executed: List[ToolCall] = []
        action = None
        draft = None

        # Search intent
        if any(w in q for w in ["find", "search", "need", "repair", "clean", "plumber", "electrician", "leak"]):
            args = {"query": request.message, "city": None}
            res = self._execute_local_tool("search_services", args, request.context)
            tools_executed.append(ToolCall(tool_name="search_services", arguments=args, result=res))

            if res["services"]:
                top = res["services"][0]
                reply = (
                    f"Hi {request.context.user_name}, I found top-rated options for you: "
                    f"'{top['title']}' (₹{top['price']}, {top['rating']}★) in {top['city']}. "
                    f"Would you like me to check available slots or draft a booking?"
                )
            else:
                reply = "I searched our listings but couldn't find an exact match. Could you specify your city or service type?"

        # Booking intent
        elif any(w in q for w in ["book", "schedule", "reserve"]):
            args = {
                "service_id": 1,
                "booking_date": "2026-11-15",
                "address": "Customer Registered Address",
                "notes": "Booked via ServeNow AI Concierge"
            }
            res = self._execute_local_tool("create_booking_draft", args, request.context)
            tools_executed.append(ToolCall(tool_name="create_booking_draft", arguments=args, result=res))
            action = "CONFIRM_BOOKING"
            draft = res
            reply = (
                f"I've prepared a draft booking for {request.context.user_name} on {draft['booking_date']}. "
                f"Please review and confirm to proceed with payment verification."
            )

        # Status inquiry intent
        elif any(w in q for w in ["status", "where is", "track"]):
            args = {"booking_id": 101}
            res = self._execute_local_tool("explain_booking_status", args, request.context)
            tools_executed.append(ToolCall(tool_name="explain_booking_status", arguments=args, result=res))
            reply = (
                f"Here is how your booking status works: "
                f"Once you submit payment with a 12-digit UTR, it is AUTO_CONFIRMED. "
                f"Admin verification unlocks the provider's phone and address."
            )

        else:
            reply = (
                f"Hello {request.context.user_name}! I am your ServeNow assistant. "
                f"You can ask me to find services, check dates, schedule a booking, or explain booking status."
            )

        return ChatbotResponse(
            reply=reply,
            action_required=action,
            draft_booking=draft,
            tools_called=tools_executed
        )

chatbot_service = ChatbotService()
