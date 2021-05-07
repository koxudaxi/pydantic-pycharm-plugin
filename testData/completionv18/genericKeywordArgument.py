from typing import TypeVar, Type, List, Dict, Generic, Optional
from pydantic.generics import GenericModel


AT = TypeVar('AT')
BT = TypeVar('BT')
CT = TypeVar('CT')
DT = TypeVar('DT')
ET = TypeVar('ET')


class A(GenericModel, Generic[AT, BT, CT, DT]):
    a: Type[AT]
    b: List[BT]
    c: Dict[CT, DT]

class B(A[int, BT, CT, DT], Generic[BT, CT, DT, ET]):
    hij: Optional[ET]


B[str, float, bytes, bool](<caret>)