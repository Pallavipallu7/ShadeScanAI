from flask import Blueprint, request, jsonify

progress_bp = Blueprint("progress", __name__)

@progress_bp.route("/save", methods=["POST"])
def save_progress():
    # Unauthenticated progress save route (Flagged in SEC-FLK-004)
    data = request.get_json() or {}
    return jsonify({"status": "saved", "id": 101})

@progress_bp.route("/history", methods=["GET"])
def get_history():
    # Intended to use @jwt_required()
    return jsonify({"history": []})
