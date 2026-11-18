---
description: TZ bo'yicha bosqichni (1–10) yoki aniq vazifani dev agenti orqali bajarish
argument-hint: "<bosqich raqami | vazifa matni>"
---

`dev` agentini (Agent tool, subagent_type: "dev") ishga tushir va unga quyidagi topshiriqni ber.

Topshiriq: $ARGUMENTS

- Agar topshiriq raqam bo'lsa (1–10), bu `docs/TZ.md` 13-bo'limidagi o'sha bosqich; agent 14-bo'limdagi mos promptni bajaradi.
- Agar bo'sh bo'lsa, agent `git log` va TZ 13-bo'limiga qarab keyingi bajarilmagan bosqichni tanlaydi.
- Matn bo'lsa, o'sha vazifani TZ va CLAUDE.md qoidalariga mos bajaradi.

Agent tugagach, uning hisobotini (nima qilindi, testlar natijasi, qolgan ishlar) foydalanuvchiga qisqa qilib yetkaz.
