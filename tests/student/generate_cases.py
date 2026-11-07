#!/usr/bin/env python3
"""Deterministic independent fixture generator; run from any directory."""
import json,random
from pathlib import Path
from verify_student import reference
DIR=Path(__file__).resolve().parent
rng=random.Random(20261107)
cases=[]
def add(task,**args):cases.append({'task':task,**args})
add('ping')
for a,b in [('',''),('', 'abc'),('abc',''),('tea_time','team'),('abc','bac'),('aaaa','aa'),('ab','ba'),('abcdef','uvwxyz'),('! A!','! !')]: add('lcs',a=a,b=b)
for sizes in [[],[0],[7],[2,3,4],[1,1,1],[0,0,0],[10**9]*5,[2,0,4,0],[5,1,1,5]]:add('merge',sizes=sizes)
for w,e in [([],[]),([-5],[]),([0],[]),([4,9,6],[[0,1],[1,2]]),([1,1],[[0,1]]),([-1,-2,-3],[[0,1],[1,2]]),([0,0,0],[[0,1],[1,2]]),([10**9,10**9,10**9],[[0,1],[1,2]]),([9,-10,9,9],[[0,1],[1,2],[1,3]])]: add('tree',n=len(w),weights=w,edges=e)
for task in ('naive','kmp','rk'):
    for txt,pat in [('', ''),('abc',''),('','a'),('ab','abc'),('aaaaa','aaa'),('abababa','aba'),('aB# aB#','B#'),('abdj','ab'),('dj','ab'),('aaaa','b')]:add(task,text=txt,pattern=pat)
for txt in ['', 'a','banana','aaaaa','abcdef','abababa','abcdabcd','! ! !','ababaabababa']: add('suffix',text=txt)
for pats,txts in [([],[]),([],['','abc']),(['a'],[]),(['a'],['','aaa','aba']),(['he','she','he'],['sheshe','sheshe','']),(['a','aa','aaa'],['aaaaa']),(['bc','abc','c'],['abc','cabc'])]:add('aho',patterns=pats,texts=txts)
for _ in range(12):
    a=''.join(rng.choices('abC! ',k=rng.randrange(0,9)));b=''.join(rng.choices('abC! ',k=rng.randrange(0,9)));add('lcs',a=a,b=b)
    add('merge',sizes=[rng.randrange(0,15) for _ in range(rng.randrange(0,8))])
    n=rng.randrange(0,10); weights=[rng.randrange(-10,15) for _ in range(n)]; edges=[[i,rng.randrange(i)] for i in range(1,n)]; rng.shuffle(edges); add('tree',n=n,weights=weights,edges=edges)
    t=''.join(rng.choices('ab cd!',k=rng.randrange(0,24)));p=''.join(rng.choices('ab cd!',k=rng.randrange(0,10)))
    for task in ('naive','kmp','rk'):add(task,text=t,pattern=p)
    add('suffix',text=''.join(rng.choices('ab#',k=rng.randrange(0,18))))
    patterns=[''.join(rng.choices('ab',k=rng.randrange(1,5))) for _ in range(rng.randrange(0,9))]
    texts=[''.join(rng.choices('ab',k=rng.randrange(0,15))) for _ in range(rng.randrange(0,6))]
    add('aho',patterns=patterns,texts=texts)
for file,objects in [('inputs.jsonl',cases),('expected.jsonl',[reference(c) for c in cases])]:
    (DIR/file).write_text(''.join(json.dumps(obj,ensure_ascii=False,separators=(',',':'))+'\n' for obj in objects),encoding='utf-8')
print('Generated',len(cases),'deterministic requests and independent expected witnesses')
