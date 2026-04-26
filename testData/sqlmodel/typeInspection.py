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


def f(x: Optional[int]) -> None:
    pass

f(<warning descr="Expected type 'Optional[int]', got 'InstrumentedAttribute[Optional[int]]' instead">TableModel.id</warning>)
f(non_table_instance.id)
f(table_instance.id)
