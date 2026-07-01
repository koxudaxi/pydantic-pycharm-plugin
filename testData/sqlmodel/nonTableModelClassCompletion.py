from sqlmodel import Field, SQLModel


class NonTableModel(SQLModel):
    id: int
    name: str


NonTableModel.<caret>
