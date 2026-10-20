"""Summarise JUnit XML results: python3 scripts/test_summary.py [results-dir]"""
import glob, sys, xml.etree.ElementTree as ET

d = sys.argv[1] if len(sys.argv) > 1 else "app/build/test-results/testDebugUnitTest"
total = failed = 0
slow = []
for p in sorted(glob.glob(f"{d}/*.xml")):
    r = ET.parse(p).getroot()
    total += int(r.get("tests"))
    for c in r.iter("testcase"):
        slow.append((float(c.get("time") or 0), f"{r.get('name').split('.')[-1]}.{c.get('name')}"))
        for x in c:
            if x.tag in ("failure", "error"):
                failed += 1
                print("FAIL", r.get("name").split(".")[-1], c.get("name"), "\n    ", (x.get("message") or x.text or "")[:400])
print(f"total {total}  passed {total - failed}  failed {failed}")
for t, n in sorted(slow, reverse=True)[:5]:
    print(f"  slowest {t:7.2f}s {n}")
