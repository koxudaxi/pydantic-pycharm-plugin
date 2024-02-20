import dataclasses

from pydantic import BaseModel


class B(BaseModel):
    a: int = 1

    def f(self):
        self.<warning descr="Unresolved attribute reference 'fake' for class 'B'">fake</warning>
        self.a
b = B(a=1)
b.<warning descr="Unresolved attribute reference 'fake' for class 'B'">fake</warning>
class C(B):
   c: int = 1
   def ff(self):
        self.<warning descr="Unresolved attribute reference 'fake' for class 'C'">fake</warning>
        self.a
        self.c
c = C(a=1, c=1)
c.a
c.<warning descr="Unresolved attribute reference 'b' for class 'C'">b</warning>
