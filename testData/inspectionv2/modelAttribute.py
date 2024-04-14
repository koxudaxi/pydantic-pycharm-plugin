from typing import ClassVar
import dataclasses

from pydantic import BaseModel


class B(BaseModel):
    class Inner(BaseModel):
        user: str
    a: int = 1
    _url: str = "https://someurl"
    TEST: ClassVar[str] = "Hello World"
    def f(self):
        self._url
        self.Inner
        self.<warning descr="Unresolved attribute reference 'fake' for class 'B'">fake</warning>
        self.a
B.<warning descr="Unresolved attribute reference 'a' for class 'B'">a</warning>
b = B(a=1)
b.<warning descr="Unresolved attribute reference 'fake' for class 'B'">fake</warning>
B.Inner
class C(B):
   c: int = 1
   def ff(self):
        self.<warning descr="Unresolved attribute reference 'fake' for class 'C'">fake</warning>
        self.a
        self.c
        self._url
c = C(a=1, c=1)
c.a
C._url
c._url
c.<warning descr="Unresolved attribute reference 'b' for class 'C'">b</warning>
print(B.TEST)
print(C.TEST)
C.Inner