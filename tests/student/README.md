# Assignment 4 — Student verification and benchmark

The official template's public tests are in `tests/public.json`; these additional tests are independent student work with ChatGPT assistance disclosed in the root analysis.

## Commands (run at repository root)

```bash
python tools/run.py --setup
python tools/run.py --check
python tests/student/generate_cases.py
python tools/run.py --input tests/student/inputs.jsonl > tests/student/actual_outputs.jsonl
python tests/student/verify_student.py
python tests/student/stress_limits.py
javac --release 17 -cp Java/build -d Java/build tests/student/SearchBenchmark.java
java -Xmx384m -cp Java/build SearchBenchmark > tests/student/search_benchmark.csv
```

- `inputs.jsonl`: 170 requests, seeded fixed tests and mixed tasks; `expected.jsonl`: independently derived small-domain expected optimums and possible witnesses; `actual_outputs.jsonl`: real Java algorithm responses to these same requests, one per line.
- `generate_cases.py`: reproducible fixture generation using seed `20261107`.
- `verify_student.py`: exhaustive small-case oracles for LCS, merge and tree; direct search/suffix/Aho references; validates actual witnesses, including two valid tie answers and invalid counterexamples. It does not simply compare one arbitrary witness to another.
- `stress_limits.py` and `stress_results.txt`: stress cases at full published limits, independent closed-form optimums/invariants, Java heap 384 MiB and a 20-second timeout per task.
- `SearchBenchmark.java` and `search_benchmark.csv`: fixed-seed correctness-checked direct Java benchmark for naive/KMP/RK at three sizes and two distributions, 5 batch means and the median per method/input.
- `setup_log.txt` and `public_checks_local.txt`: actual output from official provided setup and public-check launcher. The public check reports 44 separate cases and one combined persistent-process batch.

**Note:** Java builds target release 17. Timing results came from JDK 21 executing Java 17-compatible bytecode in a shared Linux container; reruns on other hardware will produce different measurements. This is evidence from local runs, not an official grade.
