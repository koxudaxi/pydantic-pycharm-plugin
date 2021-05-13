from pydantic import BaseModel


class A(BaseModel):
    abc: str = '123'
    def __init__(self):
        pass
A()

class B(BaseModel):
    abc: str = '123'
    def __init__(self, a: float, b: int):
        pass
B(abc='123'<warning descr="null">)</warning>

class C(BaseModel):
    abc: str = '123'
    def __init__(self, a: float, b: int = 123):
        pass

C(abc='123'<warning descr="null">)</warning>


# class D(BaseModel):
#     abc: str = '123'
#     def __init__(self, *a):
#         pass
#
# C(abc='123')
#
# class E(BaseModel):
#     abc: str = '123'
#     def __init__(self, **a):
#         pass
# E(abc='123')

class F(C):
    abc: str = '123'

    def __init__(self, c: float, d: str):
        super(F, self).__init__(...)
F(abc='123'<warning descr="null">)</warning>

class G(C):
    abc: str = '123'

    def __new__(self, c: float, d: str):
        super(F, self).__init__(...)
G(abc='123')


class H(C):
    pass

H(abc='123'<warning descr="null">)</warning>
