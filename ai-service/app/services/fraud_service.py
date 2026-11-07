import logging
import numpy as np
from sklearn.ensemble import IsolationForest
from app.schemas.fraud import PaymentFeatures, FraudScoreResponse

log = logging.getLogger(__name__)

class FraudService:
    def __init__(self):
        # Initialize and pre-fit IsolationForest with synthetic baseline data
        self.model = IsolationForest(n_estimators=100, contamination=0.08, random_state=42)
        self._fit_baseline_distribution()

    def _fit_baseline_distribution(self):
        np.random.seed(42)
        # Normal customer transactions:
        # feature 0: amount_ratio (expected / amount ~ 1.0)
        # feature 1: velocity (0 - 2)
        # feature 2: account_age_hours (24 - 1000)
        # feature 3: consecutive_failures (0)
        n_samples = 300
        normal_amount_ratio = np.random.normal(1.0, 0.05, n_samples)
        normal_velocity = np.random.poisson(0.5, n_samples)
        normal_account_age = np.random.uniform(24.0, 720.0, n_samples)
        normal_failures = np.zeros(n_samples)

        X_normal = np.column_stack([
            normal_amount_ratio,
            normal_velocity,
            normal_account_age,
            normal_failures
        ])

        # A few anomalous transactions
        X_anomalous = np.array([
            [3.5, 6, 0.5, 4],
            [0.1, 8, 1.0, 5],
            [5.0, 10, 0.2, 3],
        ])

        X_train = np.vstack([X_normal, X_anomalous])
        self.model.fit(X_train)
        log.info("IsolationForest baseline model initialized.")

    def score_payment(self, features: PaymentFeatures) -> FraudScoreResponse:
        reasons = []
        rule_score = 0.0

        # Rule 1: Reference reuse (Critical risk)
        if features.is_reference_reused:
            rule_score += 0.80
            reasons.append("UPI reference ID was previously submitted (replay attack risk)")

        # Rule 2: Amount deviation
        amount_ratio = features.amount / features.expected_amount if features.expected_amount > 0 else 1.0
        if abs(amount_ratio - 1.0) > 0.01:
            rule_score += 0.35
            reasons.append(f"Payment amount (₹{features.amount}) differs from expected booking amount (₹{features.expected_amount})")

        # Rule 3: Velocity spike (>2 attempts in 10 minutes)
        if features.velocity_last_10m >= 3:
            rule_score += 0.30
            reasons.append(f"Unusually high payment frequency ({features.velocity_last_10m} attempts in last 10 minutes)")

        # Rule 4: New account
        if features.account_age_hours < 2.0:
            rule_score += 0.20
            reasons.append(f"Account created recently ({features.account_age_hours:.1f} hours old)")

        # Rule 5: Repeated failures
        if features.consecutive_failed_payments >= 3:
            rule_score += 0.25
            reasons.append(f"{features.consecutive_failed_payments} consecutive failed payment attempts detected")

        # IsolationForest Anomaly Score
        input_features = np.array([[
            amount_ratio,
            features.velocity_last_10m,
            min(720.0, features.account_age_hours),
            features.consecutive_failed_payments
        ]])

        # decision_function: lower score = more abnormal
        iso_score = self.model.decision_function(input_features)[0]
        # Map iso_score (typically -0.3 to 0.3) to 0.0 - 0.4 anomaly weight
        iso_anomaly_weight = max(0.0, min(0.40, (0.15 - iso_score) * 0.8))

        if iso_anomaly_weight > 0.25 and not reasons:
            reasons.append("IsolationForest detected behavioral multivariate anomaly in payment pattern")

        total_score = min(1.0, max(0.0, rule_score + iso_anomaly_weight))

        if total_score >= 0.65:
            risk_level = "HIGH"
            flagged = True
        elif total_score >= 0.35:
            risk_level = "MEDIUM"
            flagged = False
        else:
            risk_level = "LOW"
            flagged = False

        if not reasons:
            reasons.append("Payment features match normal legitimate transaction profile")

        return FraudScoreResponse(
            payment_id=features.payment_id,
            risk_score=round(total_score, 3),
            risk_level=risk_level,
            reasons=reasons,
            flagged_for_review=flagged
        )

fraud_service = FraudService()
