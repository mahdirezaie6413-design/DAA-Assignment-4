import java.util.*;
/** Benchmarks the same exact-search requests on all three own algorithms.
 * Run from repo root:
 * javac --release 17 -cp Java/build -d Java/build tests/student/SearchBenchmark.java
 * java -Xmx384m -cp Java/build SearchBenchmark > tests/student/search_benchmark.csv
 */
public class SearchBenchmark {
    private static volatile long sink = 0;
    @SuppressWarnings("unchecked")
    static List<Integer> exec(String algo, Map<String,Object> request) {
        Map<String,Object> out;
        switch (algo) {
            case "naive": out=Algorithms.naive(request); break;
            case "kmp": out=Algorithms.kmp(request); break;
            case "rk": out=Algorithms.rk(request); break;
            default: throw new IllegalArgumentException(algo);
        }
        return (List<Integer>)out.get("positions");
    }
    static List<Integer> oracle(String text, String pattern) {
        List<Integer> positions=new ArrayList<>();
        for (int i=0; i+pattern.length()<=text.length(); ++i)
            if (text.startsWith(pattern,i)) positions.add(i);
        return positions;
    }
    static String randomText(int n, long seed) {
        Random rng=new Random(seed);
        StringBuilder b=new StringBuilder(n);
        for(int i=0;i<n;i++) b.append((char)(32+rng.nextInt(95)));
        return b.toString();
    }
    static double measure(String algo,Map<String,Object> request,int iterations) {
        long before=System.nanoTime();
        for(int i=0;i<iterations;i++) sink += exec(algo,request).size();
        return ((double)(System.nanoTime()-before)/iterations)/1000.0; // µs/call
    }
    public static void main(String[] args) {
        int[] nValues={250,1000,3000};
        int[] mValues={25,100,400};
        String[] methods={"naive","kmp","rk"};
        long seed=20261107L;
        System.out.println("case,n,m,seed,generator,method,run1_us,run2_us,run3_us,run4_us,run5_us,median_us,correct_verified");
        for(int size=0;size<3;size++) {
            int n=nValues[size],m=mValues[size];
            for(int mode=0;mode<2;mode++) {
                String type=mode==0 ? "random" : "repetitive";
                String text=mode==0 ? randomText(n,seed+size) : "a".repeat(n);
                // Random case contains a guaranteed match, repetitive case has long near-matches.
                String pat=mode==0 ? text.substring((n-m)/3,(n-m)/3+m) : "a".repeat(m-1)+"b";
                Map<String,Object> request=Map.of("text",text,"pattern",pat);
                List<Integer> expected=oracle(text,pat);
                for(String method:methods) {
                    if(!exec(method,request).equals(expected))throw new AssertionError("Correctness mismatch: "+type+" "+method);
                    // JIT warmup, same number of calls / timing boundary for each method.
                    for(int k=0;k<300;k++) sink += exec(method,request).size();
                }
                int iterations=mode==0 ? 40 : 12;
                for(String method:methods) {
                    measure(method,request,iterations); // additional unmeasured stabilization
                    measure(method,request,iterations);
                    double[] runs=new double[5];
                    for(int r=0;r<5;r++) runs[r]=measure(method,request,iterations);
                    double[] sorted=runs.clone();Arrays.sort(sorted);
                    StringBuilder row=new StringBuilder();
                    row.append(type).append(',').append(n).append(',').append(m).append(',')
                       .append(seed+size).append(',').append(mode==0?"seeded_printable_ascii_and_substring":"a_repeat_and_near_miss")
                       .append(',').append(method);
                    for(double v:runs)row.append(',').append(String.format(Locale.ROOT,"%.3f",v));
                    row.append(',').append(String.format(Locale.ROOT,"%.3f",sorted[2])).append(",yes");
                    System.out.println(row);
                }
            }
        }
    }
}
