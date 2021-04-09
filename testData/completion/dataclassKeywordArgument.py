
from dataclasses import field, MISSING

from pydantic.dataclasses import dataclass

def dummy():
    return '123'

@dataclass
class A:
    abc: str
    cde: str = field(default=...)
    efg: str = field(default='xyz')
    hij: str = field(default_factory=lambda :'asd')
    klm: str = field(default='qwe', init=True)
    nop: str = field(default='rty', init=False)
    qrs: str = 'fgh'
    tuw: str = field(default=MISSING)
    xyz: str = field(default_factory=MISSING)
    cda: str = field(default=MISSING, default_factory=MISSING)
    edc: str = dummy()
    gef: str = field(default=unresolved)

@dataclass
class B(A):
    child: str

A(<caret>)