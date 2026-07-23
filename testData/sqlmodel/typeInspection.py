from typing import Optional

from sqlmodel import Field, SQLModel


class NonTableModel(SQLModel):
    id: int
    name: str


class ExplicitNonTableModel(SQLModel, table=False):
    id: int
    name: str


class KeywordOnlyModel(SQLModel, frozen=True):
    id: int
    name: str


class NonBooleanTableModel(SQLModel, table="true"):
    id: int
    name: str


class TableModel(SQLModel, table=True):
    id: Optional[int] = Field(None, primary_key=True)
    name: str


non_table_instance = NonTableModel(id=1, name="non_table_instance")
table_instance = TableModel(id=1, name="table_instance")
TableModel.extra = 1

omitting_unrequired_parameter = TableModel(name="omitting_unrequired_parameter")
missing_required_parameter = TableModel(id=1<warning descr="null">)</warning>
incorrect_types_for_parameters = TableModel(<warning descr="Expected type 'Optional[int]', got 'Literal[\"id\"]' instead">id="id"</warning>, <warning descr="Expected type 'str', got 'Literal[1]' instead">name=1</warning>)

def f(x: Optional[int]) -> None:
    pass

f(<warning descr="Expected type 'Optional[int]', got 'InstrumentedAttribute[Optional[int]]' instead">TableModel.id</warning>)
f(TableModel.extra)
f(TableModel.missing)
f(non_table_instance.id)
f(table_instance.id)
