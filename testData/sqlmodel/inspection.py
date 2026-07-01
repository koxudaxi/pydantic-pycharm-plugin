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


_ = NonTableModel.<warning descr="Unresolved attribute reference 'id' for class 'NonTableModel'">id</warning>
_ = ExplicitNonTableModel.<warning descr="Unresolved attribute reference 'id' for class 'ExplicitNonTableModel'">id</warning>
_ = KeywordOnlyModel.<warning descr="Unresolved attribute reference 'id' for class 'KeywordOnlyModel'">id</warning>
_ = NonBooleanTableModel.<warning descr="Unresolved attribute reference 'id' for class 'NonBooleanTableModel'">id</warning>
_ = TableModel.id
_ = TableModel.extra
_ = TableModel.<warning descr="Unresolved attribute reference 'missing' for class 'TableModel'">missing</warning>
_ = non_table_instance.id
_ = table_instance.id
