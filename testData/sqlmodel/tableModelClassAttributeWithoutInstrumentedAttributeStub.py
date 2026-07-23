from typing import Optional

from sqlmodel import Field, SQLModel


class TableModel(SQLModel, table=True):
    id: Optional[int] = Field(None, primary_key=True)


TableModel.<caret>id
