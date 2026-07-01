from sqlmodel import Field, SQLModel


class TableModel(SQLModel, table=True):
    id: int = Field(primary_key=True)


TableModel.<caret>
