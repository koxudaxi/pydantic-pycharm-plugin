from typing import Optional

from sqlmodel import Field, SQLModel


class NonTableModel(SQLModel):
    id: int
    name: str


class TableModel(SQLModel, table=True):
    id: Optional[int] = Field(None, primary_key=True)
    name: str


non_table_instance = NonTableModel(id=1, name="non_table_instance")
table_instance = TableModel(id=1, name="table_instance")

omitting_unrequired_parameter = TableModel(name="omitting_unrequired_parameter")
missing_required_parameter = TableModel(id=1<warning descr="null">)</warning>
incorrect_types_for_parameters = TableModel(<warning descr="Expected type 'Optional[int]', got 'str' instead">id="id"</warning>, <warning descr="Expected type 'str', got 'int' instead">name=1</warning>)

def f(x: Optional[int]) -> None:
    pass

f(<warning descr="Expected type 'Optional[int]', got 'InstrumentedAttribute[Optional[int]]' instead">TableModel.id</warning>)
f(non_table_instance.id)
f(table_instance.id)
