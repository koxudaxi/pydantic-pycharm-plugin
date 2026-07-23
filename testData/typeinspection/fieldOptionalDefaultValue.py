
from typing import ClassVar, Optional

from pydantic import BaseModel


def takes_int(value: int) -> None:
    pass


module_value: int = <warning descr="Expected type 'int', got 'None' instead">None</warning>
takes_int(<warning descr="Expected type 'int', got 'None' instead">None</warning>)
text: str = "value"
takes_int(<warning descr="Expected type 'int', got 'str' instead">text</warning>)


class Container:
    def __setitem__(self, key: int, value: int) -> None:
        pass


container = Container()
container[0] = <warning descr="Expected type 'int', got 'None' instead">None</warning>


class Plain:
    class_value: int = <warning descr="Expected type 'int', got 'None' instead">None</warning>
    value: int


plain = Plain()
plain.value = <warning descr="Expected type 'int', got 'None' instead">None</warning>


class A(BaseModel):
    a: int = None
    model_config: int = None
    class_var: ClassVar[int] = <warning descr="Expected type 'int', got 'None' instead">None</warning>

    def method(self) -> None:
        local: int = <warning descr="Expected type 'int', got 'None' instead">None</warning>


A(a=int(123))
