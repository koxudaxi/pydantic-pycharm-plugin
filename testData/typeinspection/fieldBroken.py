

from pydantic import BaseModel, Schema


class A(BaseModel):
    a
    b =<EOLError descr="Expression expected"></EOLError>
    c = Schema()
    d:<EOLError descr="expression expected"></EOLError>
    e:<error descr="expression expected"> </error> = '123'
A(a=int(123), b=str('123'), c=str('456'), d=str('789'))
