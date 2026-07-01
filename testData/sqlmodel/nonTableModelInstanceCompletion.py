from sqlmodel import Field, SQLModel


class NonTableModel(SQLModel):
    id: int
    name: str

non_table_instance = NonTableModel(id=1, name="non_table_instance")
non_table_instance.<caret>
