
from typing import ClassVar
from pydantic import BaseModel

class Descriptor:
    def __get__(self, instance, owner):
        return owner

class A(BaseModel):
    _abc: str = 'abc'
    __cde: str = 'abc'
    efg: ClassVar[str] = 'abc'
    class Config:
        keep_untouched = (Descriptor,)

    descriptor1 = Descriptor()

class B(A):
    _efg: str = 'abc'
    __hij: str = 'abc'
    klm: ClassVar[str] = 'abc'
    class Config:
        keep_untouched = (Descriptor,)

    descriptor2 = Descriptor()
A().<caret>