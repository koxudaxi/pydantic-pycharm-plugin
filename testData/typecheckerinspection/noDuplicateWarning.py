from pydantic import BaseModel, BaseSettings, dataclass


class Model(BaseModel):
    value: int


@dataclass
class DataclassModel:
    value: int


class Settings(BaseSettings):
    value: int


# Pydantic BaseModel: should report a single warning
Model(<warning descr="Expected type 'int', got 'str' instead">value='not_int'</warning>)

# Pydantic dataclass: should report a single warning
DataclassModel(<warning descr="Expected type 'int', got 'str' instead">value='not_int'</warning>)

# BaseSettings subclass: should report a single warning
Settings(<warning descr="Expected type 'int', got 'str' instead">value='not_int'</warning>)


# Non-Pydantic code should still be checked by the parent inspection

def func(x: int) -> None:
    pass


func(<warning descr="Expected type 'int', got 'str' instead">'string'</warning>)
