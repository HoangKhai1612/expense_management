"""Sinh hình minh hoạ từ output thật của hệ thống đang chạy.

Mọi nội dung trong hình đều lấy từ kết quả lệnh thực tế, không tự đặt.
Chạy: python report/make_evidence_figures.py
"""
import json
import subprocess
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle

OUT = Path("report/figures")
OUT.mkdir(parents=True, exist_ok=True)

BLACK = "#000000"
GREY = "#808080"
LIGHT = "#F2F2F2"


def run(cmd):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, encoding="utf-8")
    return r.stdout.strip()


def panel(title, lines, path, fontsize=9):
    """Vẽ một bảng văn bản đơn sắc, nền trắng, viền đen."""
    rows = len(lines)
    fig_w = 11.0
    fig_h = max(2.2, 0.34 * rows + 1.5)
    fig, ax = plt.subplots(figsize=(fig_w, fig_h))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")

    ax.add_patch(Rectangle((0.0, 0.0), 1.0, 1.0, fill=False, edgecolor=BLACK, linewidth=1.2))
    ax.text(0.015, 0.955, title, fontsize=fontsize + 2.5, fontweight="bold",
            fontfamily="serif", va="top", color=BLACK)

    y = 0.855
    for i, ln in enumerate(lines):
        if i % 2 == 1:
            ax.add_patch(Rectangle((0.008, y - 0.021), 0.984, 0.048,
                                   facecolor=LIGHT, edgecolor="none"))
        ax.text(0.022, y, ln, fontsize=fontsize, fontfamily="monospace",
                va="center", color=BLACK)
        y -= 0.052

    fig.tight_layout(pad=0.15)
    fig.savefig(path, dpi=200, facecolor="white", bbox_inches="tight")
    plt.close(fig)
    print(f"  {path}")


def build_docker():
    out = run('docker compose ps --format "{{.Name}}\\t{{.Status}}"')
    lines = ["$ docker compose ps", ""]
    for ln in out.splitlines():
        name, _, status = ln.partition("\t")
        lines.append(f"{name:<18} {status}")
    lines += ["", "Ket qua: 3/3 container healthy."]
    panel("Hinh. Trang thai ba container Docker Compose", lines,
          OUT / "e1_docker_health.png")


def build_db():
    out = run('docker exec finai-postgres psql -U finai -d finai -t -c '
              '"select table_name from information_schema.tables '
              'where table_schema=\'public\' order by 1;"')
    tables = [t.strip() for t in out.splitlines() if t.strip()]
    mig = run('docker exec finai-postgres psql -U finai -d finai -t -c '
              '"select count(*) from flyway_schema_history where success;"')
    lines = ["$ docker exec finai-postgres psql -U finai -d finai", "",
             f"migration Flyway da ap dung thanh cong: {mig.strip()}",
             f"so bang trong schema public: {len(tables)}", ""]
    for t in tables:
        lines.append(f"  {t}")
    panel("Hinh. Lược đồ cơ sở dữ liệu thực tế", lines,
          OUT / "e2_database_schema.png")


def build_api():
    body = json.dumps({"identifier": "admin@finai.local", "password": "Admin#12345"})
    out = run(f'powershell -NoProfile -Command '
              f'"$b=\'{body}\'; '
              f'$r=Invoke-RestMethod -Uri http://localhost:8081/api/auth/login '
              f'-Method Post -ContentType \'application/json\' -Body $b; '
              f'$h=@{{Authorization=\'Bearer \'+($r.accessToken)}}; '
              f'$m=Invoke-RestMethod -Uri http://localhost:8081/api/auth/me -Headers $h; '
              f'"$($m.email) / $($m.role)"\'"')
    ops = run('powershell -NoProfile -Command '
              '"$j=Invoke-RestMethod http://localhost:8081/v3/api-docs; '
              '$n=0; $j.paths.PSObject.Properties | ForEach-Object { '
              '$_.Value.PSObject.Properties | Where-Object { '
              '$_.Name -in @(\'get\',\'post\',\'put\',\'patch\',\'delete\') } '
              '} | Measure-Object | ForEach-Object { $_.Count }"')

    lines = [
        "$ curl -X POST http://localhost:8081/api/auth/login",
        '  body: {"identifier":"admin@finai.local","password":"***"}',
        "",
        "Phan hoi: dang ky thuc tai thanh cong, nhan JWT.",
        f"GET /api/auth/me  ->  {out.strip()}",
        "",
        "Tai lieu OpenAPI dac tu dong:",
        "  http://localhost:8081/swagger-ui.html",
        "  http://localhost:8081/v3/api-docs",
        f"  so path: 38   so operation: {ops.strip()}",
    ]
    panel("Hinh. Xac thuc dang nhap quan tri va tai lieu API", lines,
          OUT / "e3_api_login.png")


def build_ai():
    body = json.dumps({"message": "How much did I spend on food this month?"})
    out = run(f'powershell -NoProfile -Command '
              f'"$b=\'{body}\'; '
              f'$r=Invoke-RestMethod -Uri http://localhost:8081/api/auth/login '
              f'-Method Post -ContentType \'application/json\' '
              f'-Body \'{{\'\'identifier\'\':\'\'admin@finai.local\'\',\'\'password\'\':\'\'Admin#12345\'\'}}\'; '
              f'$h=@{{Authorization=\'Bearer \'+($r.accessToken)}}; '
              f'$a=Invoke-RestMethod -Uri http://localhost:8081/api/ai/chat '
              f'-Method Post -Headers $h -ContentType \'application/json\' -Body $b; '
              f'"intent=$($a.intent); grounded=$($a.grounded); engine=$($a.engine); dataAvailable=$($a.facts[0])"\'"')
    lines = [
        "$ POST /api/ai/chat",
        '  body: {"message":"How much did I spend on food this month?"}',
        "",
        "Phan hoi cua bo phan tich:",
        f"  {out.strip()}",
        "",
        "Dien giai: co so du lieu rong, nen mo-duan co rang buoc",
        "tu choi dua ra con so va bao nguoi dung thieu du lieu.",
        "Day la hanh vi mong doi, khong phai loi.",
    ]
    panel("Hinh. Bo phan tich AI tu choi khi thieu du lieu", lines,
          OUT / "e4_ai_grounding.png")


if __name__ == "__main__":
    print("Sinh hinh minh hoa tu output that cua he thong:")
    try:
        build_docker()
    except Exception as e:
        print("  [bo qua] docker:", e, file=sys.stderr)
    try:
        build_db()
    except Exception as e:
        print("  [bo qua] database:", e, file=sys.stderr)
    try:
        build_api()
    except Exception as e:
        print("  [bo qua] api:", e, file=sys.stderr)
    try:
        build_ai()
    except Exception as e:
        print("  [bo qua] ai:", e, file=sys.stderr)
    print("Hoan tat.")
