import pydantic


class Model(pydantic.BaseModel):
    url: str

    def bla(self):
        assert type(self)

