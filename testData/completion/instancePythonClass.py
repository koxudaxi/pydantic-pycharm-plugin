

from pydantic import BaseModel


class A:
    abc: str
    cde: str
    efg: str

class B(A):
    hij: str

A().<caret>