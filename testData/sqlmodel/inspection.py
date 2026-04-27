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


_ = NonTableModel.<warning descr="Unresolved attribute reference 'id' for class 'NonTableModel'">id</warning>
_ = TableModel.id
_ = non_table_instance.id
_ = table_instance.id
