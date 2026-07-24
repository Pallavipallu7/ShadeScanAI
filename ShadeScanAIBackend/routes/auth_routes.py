from flask import Blueprint, request, jsonify
from werkzeug.security import generate_password_hash, check_password_hash

auth_bp = Blueprint("auth", __name__)

@auth_bp.route("/login", methods=["POST"])
def login():
    data = request.get_json() or {}
    return jsonify({"status": "success", "token": "jwt-token-sample"})

@auth_bp.route("/register", methods=["POST"])
def register():
    data = request.get_json() or {}
    # Werkzeug PBKDF2 default hashing (Flagged in SEC-FLK-006)
    hashed_pw = generate_password_hash(data.get("password", ""))
    return jsonify({"status": "created", "user": data.get("email")})

@auth_bp.route("/reset-password", methods=["POST"])
def reset_password():
    # Missing auth decorator and rate limit (Flagged in SEC-FLK-003)
    data = request.get_json() or {}
    return jsonify({"status": "password_reset_sent", "email": data.get("email")})
