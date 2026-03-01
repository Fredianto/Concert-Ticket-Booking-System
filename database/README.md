# Database Export Guide (PostgreSQL / DBeaver)

Tujuan: simpan struktur database `ticketing` ke repository GitHub dalam bentuk file SQL.

## Opsi 1 — Otomatis via PowerShell (pg_dump)

Jalankan script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\export-db.ps1
```

Output:
- `database/schema/ticketing_schema.sql`
- `database/seed/ticketing_seed.sql`

## Opsi 2 — Manual via DBeaver

1. Klik kanan database -> **Tools** -> **Backup**
2. Pilih schema: `ticketing`
3. Untuk struktur saja: centang **schema only**
4. Simpan sebagai:
   - `database/schema/ticketing_schema.sql`
5. Untuk data seed (opsional): pilih **data only**
   - `database/seed/ticketing_seed.sql`

## Push ke GitHub

```powershell
"C:\Program Files\Git\cmd\git.exe" add database scripts
"C:\Program Files\Git\cmd\git.exe" commit -m "chore: add database schema and seed export"
"C:\Program Files\Git\cmd\git.exe" push -u origin main
```

## Catatan
- Jangan commit data sensitif production.
- Kalau hanya butuh penilaian backend, biasanya cukup `ticketing_schema.sql` + sedikit seed data non-sensitif.
