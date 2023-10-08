from typing import Generic, TypeVar, Optional, List, Tuple, Any, Dict, Type, Union

from pydantic import BaseModel
from pydantic.generics import GenericModel

DataT = TypeVar('DataT')


class Error(BaseModel):
    code: int
    message: str


class DataModel(BaseModel):
    numbers: List[int]
    people: List[str]


class Response(GenericModel, Generic[DataT]):
    data: Optional[DataT]
    error: Optional[Error]


Response[int](data=1, error=None)

Response[str](<warning descr="Expected type 'Optional[str]', got 'int' instead">data=1</warning>, error=None)

TypeX = TypeVar('TypeX')

class BaseClass(GenericModel, Generic[TypeX]):
    X: TypeX


class ChildClass(BaseClass[TypeX], Generic[TypeX]):
    # Inherit from Generic[TypeX]
    pass


ChildClass[int](X=1)

ChildClass[str](<warning descr="Expected type 'str', got 'int' instead">X=1</warning>)



TypeX = TypeVar('TypeX')
TypeY = TypeVar('TypeY')
TypeZ = TypeVar('TypeZ')


class BaseClass(GenericModel, Generic[TypeX, TypeY]):
    x: TypeX
    y: TypeY


class ChildClass(BaseClass[int, TypeY], Generic[TypeY, TypeZ]):
    z: TypeZ


# Replace TypeY by str
ChildClass[str, float](x=1, y='y', z=3.1)

ChildClass[float, bytes](<warning descr="Expected type 'int', got 'bytes' instead">x=b'1'</warning>, <warning descr="Expected type 'float', got 'LiteralString' instead">y='y'</warning>, <warning descr="Expected type 'bytes', got 'int' instead">z=1_3</warning>)

DataT = TypeVar('DataT')


class Response(GenericModel, Generic[DataT]):
    data: DataT

    @classmethod
    def __concrete_name__(cls: Type[Any], params: Tuple[Type[Any], ...]) -> str:
        return f'{params[0].__name__.title()}Response'


Response[int](data=1)

Response[str](data='a')


Response[str](<warning descr="Expected type 'str', got 'int' instead">data=1</warning>)
Response[float](<warning descr="Expected type 'float', got 'LiteralString' instead">data='a'</warning>)


T = TypeVar('T')


class InnerT(GenericModel, Generic[T]):
    inner: T


class OuterT(GenericModel, Generic[T]):
    outer: T
    nested: InnerT[T]


nested = InnerT[int](inner=1)
OuterT[int](outer=1, nested=nested)

nested = InnerT[str](inner='a')
OuterT[int](<warning descr="Expected type 'int', got 'LiteralString' instead">outer='a'</warning>, <warning descr="Expected type 'InnerT[int]', got 'InnerT[str]' instead">nested=nested</warning>)

AT = TypeVar('AT')
BT = TypeVar('BT')


class Model(GenericModel, Generic[AT, BT]):
    a: AT
    b: BT


Model(a='a', b='a')
#> a='a' b='a'

IntT = TypeVar('IntT', bound=int)
typevar_model = Model[int, IntT]
typevar_model(a=1, b=1)


typevar_model(<warning descr="Expected type 'int', got 'LiteralString' instead">a='a'</warning>, <warning descr="Expected type 'IntT', got 'LiteralString' instead">b='a'</warning>)

concrete_model = typevar_model[int]
concrete_model(a=1, b=1)


CT = TypeVar('CT')
DT = TypeVar('DT')
ET = TypeVar('ET')
FT = TypeVar('FT')

class Model(GenericModel, Generic[CT, DT, ET, FT]):
    a: Type[CT]
    b: List[DT]
    c: Dict[ET, FT]

Model[int, int, str, int](a=int, b=[2], c={'c': 3})

Model[int, int, int, int](<warning descr="Expected type 'Type[int]', got 'int' instead">a=1</warning>, <warning descr="Expected type 'List[int]', got 'int' instead">b=2</warning>, <warning descr="Expected type 'Dict[int, int]', got 'int' instead">c=3</warning>)

class Model(GenericModel, Generic[CT, DT]):
    a: List[Type[CT]]
    b: Union[List[DT], float]

Model[int, int](a=[int], b=[2])

Model[int, int](<warning descr="Expected type 'List[Type[int]]', got 'int' instead">a=1</warning>, <warning descr="Expected type 'Union[List[int], float]', got 'LiteralString' instead">b='2'</warning>)

class Model(GenericModel, Generic[CT, DT, ET, FT]):
    a: CT
    b: DT
    c: ET
    d: FT

Model[Type[int], List[Type[int]], Optional[Type[int]], Tuple[Type[int]]](a=int, b=[int], c=int, d=(int,))

Model[Type[int], List[Type[int]], Optional[Type[int]], Tuple[Type[int]]](<warning descr="Expected type 'Type[int]', got 'int' instead">a=1</warning>, <warning descr="Expected type 'List[Type[int]]', got 'List[int]' instead">b=[2]</warning>, <warning descr="Expected type 'Optional[Type[int]]', got 'List[int]' instead">c=[3]</warning>, <warning descr="Expected type 'Tuple[Type[int]]', got 'Tuple[int]' instead">d=(4, )</warning>)

class Model(GenericModel, Generic[CT, Broken]):
    a: CT
    b: Broken

Model[int, int, int](a=1, b=2)

Model[str, str, str](<warning descr="Expected type 'str', got 'int' instead">a=1</warning>, b=2)

class Model(GenericModel, Generic[CT]):
    a: CT

Model[Union[int, float]](a=1)

Model[Union[int, float]](<warning descr="Expected type 'Union[int, float]', got 'LiteralString' instead">a='1'</warning>)

class Model(GenericModel, Generic[CT, DT]):
    a: CT
    b: DT

def x(b: ET) -> ET:
    pass

Model[x(int), Optional[x(int)]](a=1, b=2)

Model[x(int), Optional[x(int)]](<warning descr="Expected type 'int', got 'LiteralString' instead">a='1'</warning>, <warning descr="Expected type 'Optional[int]', got 'LiteralString' instead">b='2'</warning>)

class Model(GenericModel, Generic[CT, DT]):
    a: CT
    b: DT

y = int

Model[y, Optional[y]](a=1, b=2)

Model[y, Optional[y]](<warning descr="Expected type 'int', got 'LiteralString' instead">a='1'</warning>, <warning descr="Expected type 'Optional[int]', got 'LiteralString' instead">b='2'</warning>)


class Model(GenericModel, Generic[CT, DT, ET, FT, aaaaaaaaaa]):
    a: Type[CT]
    b: List[aaaaa]
    c: Dict[ET, aaaaaaaa]

Model[aaaaaaaaaa, List[aaaaaa], Tuple[aaaaaaaaaa], Type[aaaaaaaaaaa]](a=int, b=[2], <warning descr="Expected type 'Dict[Tuple[Any], Any]', got 'Dict[LiteralString, int]' instead">c={'c': 3}</warning>)