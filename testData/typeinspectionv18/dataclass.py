from typing import *

import pydantic

@pydantic.dataclasses.dataclass
class MyDataclass:
    a: str
    b: int

    def func_a(self) -> str:
        return self.a

    def func_b(self) -> int:
        return self.b

    def func_c(self) -> int:
        return <warning descr="Expected type 'int', got 'str' instead">self.a</warning>

    def func_d(self) -> str:
        return <warning descr="Expected type 'str', got 'int' instead">self.b</warning>

@pydantic.dataclasses.dataclass
class ChildDataclass(MyDataclass):
    c: str
    d: int

MyDataclass(a='apple', b=1)
MyDataclass(<warning descr="Expected type 'str', got 'int' instead">a=2</warning>, <warning descr="Expected type 'int', got 'LiteralString' instead">b='orange'</warning>)

ChildDataclass(a='apple', b=1, c='berry', d=3)
ChildDataclass(<warning descr="Expected type 'str', got 'int' instead">a=2</warning>, <warning descr="Expected type 'int', got 'LiteralString' instead">b='orange'</warning>, <warning descr="Expected type 'str', got 'int' instead">c=4</warning>, <warning descr="Expected type 'int', got 'LiteralString' instead">d='cherry'</warning>)


a: MyDataclass = MyDataclass(<warning descr="null">)</warning>
b: Type[MyDataclass] = MyDataclass

c: MyDataclass = <warning descr="Expected type 'MyDataclass', got 'Type[MyDataclass]' instead">MyDataclass</warning>
d: Type[MyDataclass] = <warning descr="Expected type 'Type[MyDataclass]', got 'MyDataclass' instead">MyDataclass(<warning descr="null">)</warning></warning>

aa: Union[str, MyDataclass] = MyDataclass(<warning descr="null">)</warning>
bb: Union[str, Type[MyDataclass]] = MyDataclass

cc: Union[str, MyDataclass] = <warning descr="Expected type 'Union[str, MyDataclass]', got 'Type[MyDataclass]' instead">MyDataclass</warning>
dd: Union[str, Type[MyDataclass]] = <warning descr="Expected type 'Union[str, Type[MyDataclass]]', got 'MyDataclass' instead">MyDataclass(<warning descr="null">)</warning></warning>

aaa: ChildDataclass = ChildDataclass(<warning descr="null">)</warning>
bbb: Type[ChildDataclass] = ChildDataclass

ccc: ChildDataclass = <warning descr="Expected type 'ChildDataclass', got 'Type[ChildDataclass]' instead">ChildDataclass</warning>
ddd: Type[ChildDataclass] = <warning descr="Expected type 'Type[ChildDataclass]', got 'ChildDataclass' instead">ChildDataclass(<warning descr="null">)</warning></warning>


e: str = MyDataclass(a='apple', b=1).a
f: int = MyDataclass(a='apple', b=1).b

g: int = <warning descr="Expected type 'int', got 'str' instead">MyDataclass(a='apple', b=1).a</warning>
h: str = <warning descr="Expected type 'str', got 'int' instead">MyDataclass(a='apple', b=1).b</warning>


ee: str = ChildDataclass(a='apple', b=1, c='orange', d=2).a
ff: int = ChildDataclass(a='apple', b=1, c='orange', d=2).d

gg: int = <warning descr="Expected type 'int', got 'str' instead">ChildDataclass(a='apple', b=1, c='orange', d=2).a</warning>
hh: str = <warning descr="Expected type 'str', got 'int' instead">ChildDataclass(a='apple', b=1, c='orange', d=2).d</warning>

i: MyDataclass = MyDataclass(a='apple', b=1)
j: str = i.a
k: int = i.b

l: int = <warning descr="Expected type 'int', got 'str' instead">i.a</warning>
m: str = <warning descr="Expected type 'str', got 'int' instead">i.b</warning>


ii: ChildDataclass = ChildDataclass(a='apple', b=1, c='orange', d=2)
jj: str = i.a
kk: int = i.d

ll: int = <warning descr="Expected type 'int', got 'str' instead">ii.a</warning>
mm: str = <warning descr="Expected type 'str', got 'int' instead">ii.d</warning>

def my_fn_1() -> MyDataclass:
    return MyDataclass(<warning descr="null">)</warning>

def my_fn_2() -> Type[MyDataclass]:
    return MyDataclass

def my_fn_3() -> MyDataclass:
    return <warning descr="Expected type 'MyDataclass', got 'Type[MyDataclass]' instead">MyDataclass</warning>

def my_fn_4() -> Type[MyDataclass]:
    return <warning descr="Expected type 'Type[MyDataclass]', got 'MyDataclass' instead">MyDataclass(<warning descr="null">)</warning></warning>

def my_fn_5() -> Union[str, MyDataclass]:
    return MyDataclass(<warning descr="null">)</warning>

def my_fn_6() -> Type[str, MyDataclass]:
    return MyDataclass

def my_fn_7() -> Union[str, MyDataclass]:
    return <warning descr="Expected type 'Union[str, MyDataclass]', got 'Type[MyDataclass]' instead">MyDataclass</warning>

def my_fn_8() -> Union[str, Type[MyDataclass]]:
    return <warning descr="Expected type 'Union[str, Type[MyDataclass]]', got 'MyDataclass' instead">MyDataclass(<warning descr="null">)</warning></warning>

def my_fn_9() -> ChildDataclass:
    return ChildDataclass(<warning descr="null">)</warning>

def my_fn_10() -> Type[ChildDataclass]:
    return ChildDataclass

def my_fn_11() -> ChildDataclass:
    return <warning descr="Expected type 'ChildDataclass', got 'Type[ChildDataclass]' instead">ChildDataclass</warning>

def my_fn_12() -> Type[ChildDataclass]:
    return <warning descr="Expected type 'Type[ChildDataclass]', got 'ChildDataclass' instead">ChildDataclass(<warning descr="null">)</warning></warning>

def my_fn_13() -> Union[str, ChildDataclass]:
    return ChildDataclass(<warning descr="null">)</warning>

def my_fn_14() -> Type[str, ChildDataclass]:
    return ChildDataclass

def my_fn_7() -> Union[str, ChildDataclass]:
    return <warning descr="Expected type 'Union[str, ChildDataclass]', got 'Type[ChildDataclass]' instead">ChildDataclass</warning>

def my_fn_8() -> Union[str, Type[ChildDataclass]]:
    return <warning descr="Expected type 'Union[str, Type[ChildDataclass]]', got 'ChildDataclass' instead">ChildDataclass(<warning descr="null">)</warning></warning>
