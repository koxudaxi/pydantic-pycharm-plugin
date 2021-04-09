
from pydantic import BaseModel
from pydantic.types import conlist

class A(BaseModel):
    abc: conlist()
    cde: conlist(str)
    efg: conlist(item_type=str)
    hij: conlist(List[str])
A(<caret>)