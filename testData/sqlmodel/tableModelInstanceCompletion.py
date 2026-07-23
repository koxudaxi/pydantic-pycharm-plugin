from sqlmodel import Field, SQLModel

class TableModel(SQLModel, table=True):
    id: int = Field(primary_key=True)
    name: str


table_instance = TableModel(id=1, name="table_instance")
table_instance.<caret>