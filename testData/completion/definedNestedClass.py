from builtins import *
from pydantic import BaseModel

class A(BaseModel):
    class Config:
        pass
    <caret>