// GeoNames cities15000.txt → app-namoz-hifz/src/main/assets/cities.json
//
// 1. https://download.geonames.org/export/dump/cities15000.zip ni yuklab oching (CC BY 4.0).
// 2. node tools/geonames-to-json.mjs path/to/cities15000.txt
//
// Ustunlar: https://download.geonames.org/export/dump/readme.txt
import { readFileSync, writeFileSync } from "node:fs";

const input = process.argv[2];
if (!input) {
  console.error("Foydalanish: node tools/geonames-to-json.mjs cities15000.txt");
  process.exit(1);
}

const cities = readFileSync(input, "utf8")
  .split("\n")
  .filter(Boolean)
  .map((line) => line.split("\t"))
  .map((c) => ({
    n: c[2], // asciiname
    c: c[8], // davlat kodi
    la: Number(Number(c[4]).toFixed(4)),
    lo: Number(Number(c[5]).toFixed(4)),
    tz: c[17],
    p: Number(c[14]),
  }))
  .filter((c) => c.n && c.tz && c.p >= 15000)
  .sort((a, b) => b.p - a.p);

const out = new URL("../app-namoz-hifz/src/main/assets/cities.json", import.meta.url);
writeFileSync(out, "[\n" + cities.map((c) => JSON.stringify(c)).join(",\n") + "\n]\n");
console.log(`${cities.length} ta shahar yozildi: ${out.pathname}`);
