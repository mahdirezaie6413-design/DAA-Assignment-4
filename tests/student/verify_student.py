#!/usr/bin/env python3
"""Independent small-domain oracles + witness checks for Assignment 4.

Run from repository root: python tests/student/verify_student.py
Verifies stored actual outputs (which must come from Java, not from this script).
"""
from __future__ import annotations
import json
from functools import lru_cache
from pathlib import Path

DIR = Path(__file__).resolve().parent

def is_subsequence(s: str, text: str) -> bool:
    it = iter(text)
    return all(any(ch == c for c in it) for ch in s)

def lcs_reference(a: str, b: str) -> dict:
    # Exhaustive small-case witness oracle, independent of student's DP.
    shorter = a if len(a) <= len(b) else b
    longer = b if shorter is a else a
    best = ""
    for mask in range(1 << len(shorter)):
        chosen = ''.join(c for i, c in enumerate(shorter) if mask >> i & 1)
        if len(chosen) > len(best) and is_subsequence(chosen, longer):
            best = chosen
    return {'length': len(best), 'sequence': best}

def merge_reference(sizes: list[int]) -> dict:
    n = len(sizes)
    @lru_cache(None)
    def solve(l, r):
        if l == r: return (0, [])
        choices=[]
        for k in range(l, r):
            left, lp = solve(l, k)
            right, rp = solve(k + 1, r)
            choices.append((left + right + sum(sizes[l:r + 1]), lp + rp + [[l, k, r]]))
        return min(choices, key=lambda x: x[0])
    if n < 2: return {'cost': 0, 'plan': []}
    cost, plan = solve(0, n - 1)
    return {'cost': cost, 'plan': plan}

def tree_reference(weights: list[int], edges: list[list[int]]) -> dict:
    n=len(weights); best=0; witness=[]
    for mask in range(1 << n):
        if any(mask >> u & 1 and mask >> v & 1 for u, v in edges): continue
        weight=sum(w for i,w in enumerate(weights) if mask >> i & 1)
        if weight>best:
            best=weight; witness=[i for i in range(n) if mask >> i & 1]
    return {'weight': best, 'selected': witness}

def search_reference(text: str, pattern: str) -> dict:
    return {'positions': [i for i in range(len(text)+1) if text[i:i+len(pattern)] == pattern]}

def suffix_reference(text: str) -> dict:
    sa=sorted(range(len(text)), key=lambda i: text[i:])
    lcp=[0]*len(text)
    for i in range(1,len(text)):
        a,b=sa[i-1],sa[i]; k=0
        while a+k<len(text) and b+k<len(text) and text[a+k]==text[b+k]: k+=1
        lcp[i]=k
    mx=max(lcp, default=0)
    rep=text[sa[lcp.index(mx)]:sa[lcp.index(mx)]+mx] if mx else ''
    return {'sa':sa,'lcp':lcp,'repeated':rep}

def aho_reference(patterns: list[str], texts: list[str]) -> dict:
    return {'counts': [[len(search_reference(t,p)['positions']) for p in patterns] for t in texts]}

def reference(req: dict) -> dict:
    task=req['task']
    if task=='lcs': return lcs_reference(req['a'],req['b'])
    if task=='merge': return merge_reference(req['sizes'])
    if task=='tree': return tree_reference(req['weights'],req['edges'])
    if task in ('naive','kmp','rk'): return search_reference(req['text'],req['pattern'])
    if task=='suffix': return suffix_reference(req['text'])
    if task=='aho': return aho_reference(req['patterns'],req['texts'])
    if task=='ping': return {'status':'ready'}
    raise ValueError(task)

def verify(req:dict, act:dict, expected:dict) -> bool:
    task=req['task']
    if task=='ping': return act == expected
    if task=='lcs':
        s=act.get('sequence')
        return isinstance(s,str) and type(act.get('length')) is int and len(s)==act['length']==expected['length'] and is_subsequence(s,req['a']) and is_subsequence(s,req['b'])
    if task=='merge':
        if type(act.get('cost')) is not int or not isinstance(act.get('plan'),list): return False
        blocks=[(i,i) for i in range(len(req['sizes']))]; cost=0
        for part in act['plan']:
            if not (isinstance(part,list) and len(part)==3 and all(type(i) is int for i in part)): return False
            l,k,r=part
            if (l,k) not in blocks:return False
            i=blocks.index((l,k))
            if i+1==len(blocks) or blocks[i+1]!=(k+1,r): return False
            cost+=sum(req['sizes'][l:r+1]); blocks[i:i+2]=[(l,r)]
        return cost==act['cost']==expected['cost'] and len(act['plan'])==max(0,len(req['sizes'])-1)
    if task=='tree':
        chosen=act.get('selected')
        if not isinstance(chosen,list) or type(act.get('weight')) is not int: return False
        if not all(type(i) is int and 0 <= i <req['n'] for i in chosen) or len(chosen)!=len(set(chosen)):return False
        s=set(chosen)
        return not any(u in s and v in s for u,v in req['edges']) and sum(req['weights'][i] for i in s)==act['weight']==expected['weight']
    if task in ('naive','kmp','rk'):
        return act.get('positions')==expected['positions']
    if task=='suffix':
        s=act.get('repeated')
        return act.get('sa')==expected['sa'] and act.get('lcp')==expected['lcp'] and isinstance(s,str) and len(s)==len(expected['repeated']) and (s=='' or req['text'].count(s)>=2 or sum(req['text'].startswith(s,i) for i in range(len(req['text'])))>=2)
    if task=='aho':return act.get('counts')==expected['counts']
    return False

def lines(p): return [json.loads(s) for s in p.read_text(encoding='utf-8').splitlines() if s.strip()]

def main():
    requests=lines(DIR/'inputs.jsonl'); expected=lines(DIR/'expected.jsonl'); actual=lines(DIR/'actual_outputs.jsonl')
    assert len(requests)==len(expected)==len(actual), 'Request / expected / actual line mismatch'
    for i,(x,e,a) in enumerate(zip(requests,expected,actual),1):
        independent=reference(x)
        assert verify(x,e,independent), f'Expected fixture invalid at {i}: {x}'
        assert verify(x,a,independent), f'Java output invalid at {i}: {x} -> {a}'
    # Explicit alternative-valid and invalid witnesses, not equality-to-one-optimum.
    tie={'task':'lcs','a':'ab','b':'ba'}
    assert verify(tie, {'length':1,'sequence':'a'}, {'length':1})
    assert verify(tie, {'length':1,'sequence':'b'}, {'length':1})
    assert not verify(tie, {'length':2,'sequence':'ab'}, {'length':1})
    tied_tree={'task':'tree','n':2,'weights':[1,1],'edges':[[0,1]]}
    assert verify(tied_tree,{'weight':1,'selected':[0]}, {'weight':1})
    assert verify(tied_tree,{'weight':1,'selected':[1]}, {'weight':1})
    assert not verify(tied_tree,{'weight':2,'selected':[0,1]}, {'weight':1})
    tie_merge={'task':'merge','sizes':[1,1,1]}
    assert verify(tie_merge,{'cost':5,'plan':[[0,0,1],[0,1,2]]},{'cost':5})
    assert verify(tie_merge,{'cost':5,'plan':[[1,1,2],[0,0,2]]},{'cost':5})
    assert not verify(tie_merge,{'cost':5,'plan':[[0,0,2],[0,0,1]]},{'cost':5})
    print(f'PASS {len(requests)} independent small-domain fixtures + alternate valid/invalid witnesses')

if __name__=='__main__':main()
