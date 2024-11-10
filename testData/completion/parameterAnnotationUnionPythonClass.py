
from typing import Union

from pydantic import BaseModel


class A:
    abc: str
    cde: str = 'abc'
    efg: str = 'abc'


def get_a(a: Union[A, str]):
    a.<caret>