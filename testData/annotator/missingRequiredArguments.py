import pydantic


class NormalModel(pydantic.BaseModel):
    value: str


class NoqaModel(pydantic.BaseModel):
    value: str


class KeywordArgumentsModel(pydantic.BaseModel):
    value: str


NormalModel(<warning descr="null">)</warning>
NoqaModel()  # noqa
values = {}
KeywordArgumentsModel(**values)
