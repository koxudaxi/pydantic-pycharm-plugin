from builtins import *

from pydantic import BaseModel, BaseSettings


class A(BaseSettings):
    b: str


A().<caret>
