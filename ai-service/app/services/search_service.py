import logging
import re
from typing import List, Optional, Tuple
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from app.schemas.search import ServiceDocument, SearchResultItem, SmartSearchResponse

log = logging.getLogger(__name__)

# Category keywords mapping for domain intelligence
CATEGORY_KEYWORDS = {
    "Plumbing": ["leak", "tap", "pipe", "sink", "drain", "clog", "faucet", "water", "flush", "plumber"],
    "Electrical": ["switch", "wire", "spark", "fuse", "short circuit", "fan", "light", "mcb", "electrician", "socket"],
    "Cleaning": ["clean", "dust", "mop", "sweep", "deep clean", "bathroom", "sofa", "kitchen cleaning", "sanitize"],
    "Appliance Repair": ["fridge", "refrigerator", "washing machine", "microwave", "ac", "air conditioner", "oven", "tv"],
    "Painting": ["paint", "whitewash", "wall", "primer", "waterproofing", "distemper", "color"],
    "Carpentry": ["wood", "door", "furniture", "lock", "cabinet", "shelf", "hinge", "carpenter", "table", "chair"]
}

class SearchService:
    def __init__(self):
        self.documents: List[ServiceDocument] = []
        self.vectorizer: Optional[TfidfVectorizer] = None
        self.doc_vectors = None
        self._initialize_default_docs()

    def _initialize_default_docs(self):
        # Default seed listings for immediate out-of-the-box search
        defaults = [
            ServiceDocument(id=1, title="Emergency Pipe Leak Repair", category="Plumbing", description="Quick fix for broken pipes, leaking faucets, and overflowing drains.", city="Bengaluru", price=399.0, rating=4.8),
            ServiceDocument(id=2, title="Full Home Deep Cleaning", category="Cleaning", description="Intensive sanitization and scrubbing for bathrooms, kitchen, and living rooms.", city="Bengaluru", price=1499.0, rating=4.9),
            ServiceDocument(id=3, title="Ceiling Fan & Switchboard Repair", category="Electrical", description="Fix flickering lights, faulty sockets, and install modern switches.", city="Bengaluru", price=299.0, rating=4.7),
            ServiceDocument(id=4, title="AC Service & Gas Refill", category="Appliance Repair", description="Split and window AC cooling check, deep coil cleaning, and coolant refill.", city="Bengaluru", price=899.0, rating=4.6),
            ServiceDocument(id=5, title="Interior Wall Painting", category="Painting", description="Premium emulsion finish with wall putty and crack sealing.", city="Bengaluru", price=4500.0, rating=4.7),
            ServiceDocument(id=6, title="Custom Woodwork & Door Repair", category="Carpentry", description="Smooth hinges, new locks installation, and wardrobe shelf fixes.", city="Bengaluru", price=550.0, rating=4.9),
        ]
        self.sync_index(defaults)

    def sync_index(self, services: List[ServiceDocument]):
        self.documents = list(services)
        if not self.documents:
            self.vectorizer = None
            self.doc_vectors = None
            return

        corpus = [
            f"{doc.title} {doc.category} {doc.description} {doc.city}"
            for doc in self.documents
        ]
        self.vectorizer = TfidfVectorizer(ngram_range=(1, 2), stop_words="english")
        self.doc_vectors = self.vectorizer.fit_transform(corpus)
        log.info("Search index synced with %d documents", len(self.documents))

    def detect_category(self, query: str) -> Optional[str]:
        q_lower = query.lower()
        best_cat = None
        best_matches = 0
        for cat, keywords in CATEGORY_KEYWORDS.items():
            matches = sum(1 for kw in keywords if re.search(r"\b" + re.escape(kw) + r"\b", q_lower))
            if matches > best_matches:
                best_matches = matches
                best_cat = cat
        return best_cat if best_matches > 0 else None

    def search(self, query: str, city: Optional[str] = None, limit: int = 10) -> SmartSearchResponse:
        if not self.documents or not self.vectorizer:
            return SmartSearchResponse(query=query, detected_category=None, results=[], total=0)

        detected_category = self.detect_category(query)
        q_vec = self.vectorizer.transform([query])
        similarities = cosine_similarity(q_vec, self.doc_vectors).flatten()

        scored_results: List[Tuple[float, ServiceDocument]] = []
        for idx, doc in enumerate(self.documents):
            # City filter (case-insensitive substring/match)
            if city and city.strip():
                if city.strip().lower() != doc.city.strip().lower():
                    continue

            base_sim = float(similarities[idx])
            boost = 0.0

            # Semantic category match boost
            if detected_category and doc.category.lower() == detected_category.lower():
                boost += 0.35

            # Direct word overlap boost
            q_words = set(query.lower().split())
            doc_text = f"{doc.title} {doc.description}".lower()
            if any(w in doc_text for w in q_words if len(w) > 3):
                boost += 0.15

            # Quality/rating factor
            rating_factor = (doc.rating / 5.0) * 0.1

            final_score = min(1.0, base_sim + boost + rating_factor)
            if final_score > 0.05 or (detected_category and doc.category.lower() == detected_category.lower()):
                scored_results.append((final_score, doc))

        # Sort descending by score
        scored_results.sort(key=lambda x: x[0], reverse=True)

        items = [
            SearchResultItem(
                id=doc.id,
                title=doc.title,
                category=doc.category,
                description=doc.description,
                city=doc.city,
                price=doc.price,
                rating=doc.rating,
                relevance_score=round(score, 3)
            )
            for score, doc in scored_results[:limit]
        ]

        return SmartSearchResponse(
            query=query,
            detected_category=detected_category,
            results=items,
            total=len(items)
        )

search_service = SearchService()
