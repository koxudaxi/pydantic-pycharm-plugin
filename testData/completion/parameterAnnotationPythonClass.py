from builtins import *
from pydantic import BaseModel


class A:
    abc: str
    cde: str = str('abc')
    efg: str = str('abc')


def get_a(a: A):
    a.<caret>