from flask import Blueprint, jsonify

dashboard_bp = Blueprint("dashboard", __name__)

@dashboard_bp.route("/stats", methods=["GET"])
def get_stats():
    # Unauthenticated dashboard endpoint (Flagged)
    return jsonify({"scans_count": 142, "patients_count": 38})
