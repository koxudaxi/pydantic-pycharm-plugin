from pydantic import BaseModel, create_model

DynamicFoobarModel = create_model('DynamicFoobarModel', foo=(str, ...), bar=123)

class StaticFoobarModel(BaseModel):
    foo: str
    bar: int = 123


DynamicFoobarModel(foo='name', bar=123)
DynamicFoobarModel(<warning descr="Expected type 'str', got 'int' instead">foo=123</warning>, <warning descr="Expected type 'int', got 'str' instead">bar='name'</warning>)

BarModel = create_model(
    model_name='BarModel',
    apple='russet',
    banana='yellow',
    __base__=StaticFoobarModel,
)

BarModel(foo='name', bar=123, apple='green', banana='red')
BarModel(<warning descr="Expected type 'str', got 'int' instead">foo=123</warning>, <warning descr="Expected type 'int', got 'str' instead">bar='name'</warning>, <warning descr="Expected type 'str', got 'int' instead">apple=123</warning>, <warning descr="Expected type 'str', got 'int' instead">banana=456</warning>)

model_name = 'DynamicBarModel'
DynamicBarModel = create_model(
    model_name,
    apple='russet',
    banana='yellow',
    __base__=DynamicFoobarModel,
)

DynamicBarModel(foo='name', bar=123, apple='green', banana='red')
DynamicBarModel(<warning descr="Expected type 'str', got 'int' instead">foo=123</warning>, <warning descr="Expected type 'int', got 'str' instead">bar='name'</warning>, <warning descr="Expected type 'str', got 'int' instead">apple=123</warning>, <warning descr="Expected type 'str', got 'int' instead">banana=456</warning>)

DynamicModifiedBarModel = create_model('DynamicModifiedFoobarModel', foo=(int, ...), bar='abc', __base__=DynamicBarModel)

DynamicModifiedBarModel(foo=456, bar='efg', apple='green', banana='red')
DynamicModifiedBarModel(<warning descr="Expected type 'int', got 'str' instead">foo='123'</warning>, <warning descr="Expected type 'str', got 'int' instead">bar=456</warning>, <warning descr="Expected type 'str', got 'int' instead">apple=123</warning>, <warning descr="Expected type 'str', got 'int' instead">banana=456</warning>)


DynamicBrokenModel = create_model(
    'DynamicBrokenModel',
    apple='russet',
    banana='yellow',
    __base__=BrokenBase,
)

DynamicBrokenModel(foo='name', bar=123, apple='green', banana='red')

class PythonClass:
    pass

DynamicBrokenModel = create_model(
    'DynamicBrokenModel',
    <warning descr="Expected type 'Optional[Type[BaseModel]]', got 'Type[PythonClass]' instead">__base__=PythonClass</warning>,
)

DynamicBrokenModel()