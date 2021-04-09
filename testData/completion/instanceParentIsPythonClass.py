

from pydantic import BaseModel


class A:
    abc: str
    cde: str
    efg: str

class B(BaseModel, A):
    hij: str

B().<caret>