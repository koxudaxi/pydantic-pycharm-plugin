
from typing import Union, List

from pydantic import BaseModel


class A(BaseModel):
    a: List[str]
    b: List[List[str]]

a_int: List[int] = [int('123')]
a_str: List[str] = [str('123')]
b_int: List[List[int]] = [[int('123')]]
b_str: List[List[str]] = [[str('123')]]
A(a=a_str, b=b_str)
A(<warning descr="Field is of type 'List[str]', 'List[int]' may not be parsable to 'List[str]'">a=a_int</warning>, <warning descr="Field is of type 'List[List[str]]', 'List[List[int]]' may not be parsable to 'List[List[str]]'">b=b_int</warning>)
