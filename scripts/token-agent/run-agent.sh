#!/bin/bash
# Script khởi động Local USB Token Signing Agent
cd "$(dirname "$0")"
echo "Đang khởi động Local USB Token Signing Agent tại http://127.0.0.1:8989 ..."
python3 local_signing_agent.py

