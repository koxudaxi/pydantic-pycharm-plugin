import typing

from .main import BaseModel

RootModelRootType = typing.TypeVar('RootModelRootType')



class RootModel(BaseModel, typing.Generic[RootModelRootType]):
    root: RootModelRootType