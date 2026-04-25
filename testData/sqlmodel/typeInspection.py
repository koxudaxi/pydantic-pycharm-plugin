from sqlmodel import Field, SQLModel


class NonTableModel(SQLModel):
    id: int
    name: str


class TableModel(SQLModel, table=True):
    id: int | None = Field(None, primary_key=True)
    name: str


non_table_instance = NonTableModel(id=1, name="non_table_instance")
table_instance = TableModel(id=1, name="table_instance")


def f(x: int | None) -> None:
    pass

f(<warning descr="Expected type 'int | None', got 'InstrumentedAttribute[int | None]' instead">TableModel.id</warning>)
f(non_table_instance.id)
f(table_instance.id)
