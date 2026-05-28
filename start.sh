#!/bin/bash
# ============================================
#  MarkLive 一键启动脚本 (macOS / Linux)
#  自动启动 Java 后端 + Vue 前端，并打开浏览器
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_PID=""
FRONTEND_PID=""

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 优雅退出：Ctrl+C 时同时杀掉前后端进程
cleanup() {
    echo ""
    echo -e "${YELLOW}[MarkLive] 正在关闭服务...${NC}"

    if [ -n "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        kill "$FRONTEND_PID" 2>/dev/null
        echo -e "${GREEN}[MarkLive] 前端已停止 (PID: $FRONTEND_PID)${NC}"
    fi

    if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        kill "$BACKEND_PID" 2>/dev/null
        echo -e "${GREEN}[MarkLive] 后端已停止 (PID: $BACKEND_PID)${NC}"
    fi

    # 确保端口释放
    lsof -ti:8080 2>/dev/null | xargs kill -9 2>/dev/null || true
    lsof -ti:3000 2>/dev/null | xargs kill -9 2>/dev/null || true

    echo -e "${GREEN}[MarkLive] 所有服务已关闭，再见！${NC}"
    exit 0
}

trap cleanup SIGINT SIGTERM

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   MarkLive 一键启动${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# ==================== 1. 启动后端 ====================
echo -e "${YELLOW}[1/3] 启动 Java 后端 (Spring Boot :8080)...${NC}"
cd "$SCRIPT_DIR"

mvn spring-boot:run -q &
BACKEND_PID=$!
echo -e "${GREEN}  ✓ 后端进程已启动 (PID: $BACKEND_PID)，等待就绪...${NC}"

# 等待后端就绪（最多等待 30 秒）
for i in $(seq 1 30); do
    if curl -s http://localhost:8080/api/markdown/scan?folderPath=/tmp > /dev/null 2>&1; then
        echo -e "${GREEN}  ✓ 后端已就绪！${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}  ✗ 后端启动超时，请检查日志。${NC}"
        cleanup
    fi
    sleep 1
done

# ==================== 2. 启动前端 ====================
echo ""
echo -e "${YELLOW}[2/3] 启动 Vue 前端 (Vite :3000)...${NC}"
cd "$SCRIPT_DIR/frontend"

if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}  ⏳ 检测到缺少 node_modules，正在自动安装依赖...${NC}"
    npm install
    echo -e "${GREEN}  ✓ 依赖安装完成${NC}"
fi

npx vite --host 0.0.0.0 --port 3000 &
FRONTEND_PID=$!
echo -e "${GREEN}  ✓ 前端进程已启动 (PID: $FRONTEND_PID)${NC}"

# 等待前端就绪
sleep 3

# ==================== 3. 打开浏览器 ====================
echo ""
echo -e "${YELLOW}[3/3] 打开浏览器...${NC}"

if command -v open &> /dev/null; then
    open http://localhost:3000
elif command -v xdg-open &> /dev/null; then
    xdg-open http://localhost:3000
fi
echo -e "${GREEN}  ✓ 浏览器已打开${NC}"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   MarkLive 启动完成！${NC}"
echo -e "${GREEN}   后端: http://localhost:8080${NC}"
echo -e "${GREEN}   前端: http://localhost:3000${NC}"
echo -e "${GREEN}   按 Ctrl+C 停止所有服务${NC}"
echo -e "${GREEN}========================================${NC}"

# 保持脚本运行，等待用户按 Ctrl+C
wait
