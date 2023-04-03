from pydantic import BaseModel


class <error descr="__root__ models are no longer supported in v2; a migration guide will be added in the near future">A</error>(BaseModel):
    __root__ = 'xyz'


class B(BaseModel):
    a = 'xyz'

class C:
    __root__ = 'xyz'
    e = 'xyz'

def d():
    __root__ = 'xyz'
    g = 'xyz'

