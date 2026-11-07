#!/usr/bin/env python3
"""Maximum-size correctness and runtime checks, with independent invariants.
Run from root after setup: python tests/student/stress_limits.py
Outputs a per-case log; Java heap capped at 384 MiB, timeout 20 s per task.
"""
import json,random,subprocess,time
from pathlib import Path
P=Path(__file__).resolve().parents[2]
COMMAND=['java','-Xmx384m','-cp',str(P/'Java'/'build'),'Main']
LOG=P/'tests'/'student'/'stress_results.txt'
rows=[]
def call(label, req, check):
    data=json.dumps(req,separators=(',',':'))+'\n'
    start=time.perf_counter()
    result=subprocess.run(COMMAND,input=data,text=True,capture_output=True,timeout=20,check=True)
    elapsed=time.perf_counter()-start
    assert len(result.stdout.splitlines())==1, label+': unexpected JSONL line count'
    obj=json.loads(result.stdout)
    check(obj)
    line=f'PASS {label}: {elapsed:.3f}s, {len(data)} request bytes, {len(result.stdout)} result bytes; verified optimality/witness or exact results'
    rows.append(line); print(line,flush=True)

def assert_lcs(o):
    assert o['length']==1000 and o['sequence']=='a'*1000
call('LCS maximum 1000x1000',{'task':'lcs','a':'a'*1000,'b':'a'*1000},assert_lcs)

def verify_merge(o):
    n=200; blocks=[(i,i) for i in range(n)]; total=0
    for l,k,r in o['plan']:
        i=blocks.index((l,k));assert i+1<len(blocks) and blocks[i+1]==(k+1,r)
        blocks[i:i+2]=[(l,r)];total+=(r-l+1)*10**9
    # Independent theoretical optimum: sum of depths in minimum-height full binary tree
    # with n equally-weighted leaves: n*floor(log2(n)) + 2*(n-2^floor(log2(n))).
    bits=n.bit_length()-1
    optimal=(n*bits+2*(n-2**bits))*10**9
    assert len(o['plan'])==n-1 and blocks==[(0,199)] and o['cost']==total==optimal,(total,optimal)
call('Merge 200 adjacent 1e9 blocks',{'task':'merge','sizes':[10**9]*200},verify_merge)

n=100000; w=[10**9 if i%2==0 else -10**9 for i in range(n)]
def verify_tree(o):
    selected=o['selected'];assert len(set(selected))==len(selected)
    assert o['weight']==50000*10**9
    assert all(0<=v<n for v in selected) and all(abs(a-b)!=1 for a,b in zip(sorted(selected),sorted(selected)[1:]))
    assert sum(w[v] for v in selected)==o['weight']
call('Tree deep chain 100000 vertices',{'task':'tree','n':n,'weights':w,'edges':[[i,i+1] for i in range(n-1)]},verify_tree)

m=100000; length=200000
def verify_kmp(o):
    pos=o['positions'];assert len(pos)==length-m+1 and pos[0]==0 and pos[-1]==length-m and all(i==x for i,x in enumerate(pos))
call('KMP max 200000 text / 100000 pattern',{'task':'kmp','text':'a'*length,'pattern':'a'*m},verify_kmp)
def assert_empty(x): assert x==[]
call('RK max 10000 x 2000 product',{'task':'rk','text':'a'*10000,'pattern':'a'*1999+'b'},lambda o:assert_empty(o['positions']))

def verify_suffix(o):
    n=50000
    assert o['sa']==list(range(n-1,-1,-1))
    assert o['lcp']==list(range(n))
    assert o['repeated']=='a'*(n-1)
call('Suffix 50000 identical chars',{'task':'suffix','text':'a'*50000},verify_suffix)

rng=random.Random(20261107)
alpha='BCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
patterns=[''.join(rng.choice(alpha) for _ in range(10)) for _ in range(10000)]
texts=['a'*2000]*100

def verify_aho(o):
    assert len(o['counts'])==100
    assert all(len(row)==10000 and all(v==0 for v in row) for row in o['counts'])
call('Aho max 10000x10 dictionary / 100x2000 text',{'task':'aho','patterns':patterns,'texts':texts},verify_aho)

patterns=['a'*i for i in range(1,11)]*1000
texts=['a'*200000]
def verify_dense_aho(o):
    assert len(o['counts'])==1 and len(o['counts'][0])==10000
    for i,val in enumerate(o['counts'][0]):assert val==200000-(i%10+1)+1
call('Aho 10000 dense duplicates / 200000 text',{'task':'aho','patterns':patterns,'texts':texts},verify_dense_aho)

LOG.write_text('\n'.join(rows)+'\n',encoding='utf-8')
print(f'PASS all {len(rows)} scale/stress cases',flush=True)
